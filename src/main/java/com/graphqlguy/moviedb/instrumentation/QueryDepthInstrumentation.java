package com.graphqlguy.moviedb.instrumentation;

import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.DocumentAndVariables;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueryDepthInstrumentation extends SimplePerformantInstrumentation {

    private final InstrumentationProperties properties;

    @Override
    public DocumentAndVariables instrumentDocumentAndVariables(
            DocumentAndVariables documentAndVariables,
            InstrumentationExecutionParameters parameters,
            InstrumentationState state) {

        int maxDepth = properties.maxQueryDepth();
        Document document = documentAndVariables.getDocument();

        // Collect fragment definitions for resolving FragmentSpreads
        Map<String, FragmentDefinition> fragments = document.getDefinitions().stream()
                .filter(FragmentDefinition.class::isInstance)
                .map(FragmentDefinition.class::cast)
                .collect(Collectors.toMap(FragmentDefinition::getName, f -> f));

        int depth = calculateDepth(document, fragments);

        if (depth > maxDepth) {
            log.warn("Query rejected: depth {} exceeds maximum {}", depth, maxDepth);
            throw new AbortExecutionException(
                    String.format("Query depth %d exceeds maximum allowed depth of %d",
                            depth, maxDepth));
        }

        return documentAndVariables;
    }

    private int calculateDepth(Document document, Map<String, FragmentDefinition> fragments) {
        return document.getDefinitions().stream()
                .filter(OperationDefinition.class::isInstance)
                .map(OperationDefinition.class::cast)
                .mapToInt(op -> depthOf(op.getSelectionSet(), fragments, new HashSet<>()))
                .max()
                .orElse(0);
    }

    private int depthOf(SelectionSet selectionSet,
                        Map<String, FragmentDefinition> fragments,
                        Set<String> visitedFragments) {
        if (selectionSet == null) {
            return 0;
        }
        int max = 0;
        for (Selection<?> selection : selectionSet.getSelections()) {
            int depth = 0;
            if (selection instanceof Field field) {
                depth = 1 + depthOf(field.getSelectionSet(), fragments, visitedFragments);
            } else if (selection instanceof InlineFragment inlineFragment) {
                depth = depthOf(inlineFragment.getSelectionSet(), fragments, visitedFragments);
            } else if (selection instanceof FragmentSpread spread) {
                FragmentDefinition fragment = fragments.get(spread.getName());
                // this hook runs before validation, so a fragment cycle could still be present
                if (fragment != null && visitedFragments.add(spread.getName())) {
                    depth = depthOf(fragment.getSelectionSet(), fragments, visitedFragments);
                    visitedFragments.remove(spread.getName());
                }
            }
            max = Math.max(max, depth);
        }
        return max;
    }
}
