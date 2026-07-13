// Discovers what the backend's GraphQL schema currently supports, via one
// introspection query at app startup. The workshop builds the schema class by
// class, so the frontend adapts to whatever exists right now instead of
// assuming the finished API.

const INTROSPECTION = `
  query FrontendCapabilities {
    __schema {
      queryType { name }
      mutationType { name }
      subscriptionType { name }
      types {
        name
        kind
        fields {
          name
          args { name }
        }
        possibleTypes { name }
      }
    }
  }
`;

// Result statuses:
//   'ready'        - schema loaded, caps helpers usable
//   'no-graphql'   - backend responds but has no /graphql endpoint yet (pre class 1)
//   'backend-down' - nothing answered on the backend port
export async function fetchCapabilities() {
  let res;
  try {
    res = await fetch('/graphql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: INTROSPECTION }),
    });
  } catch {
    return { status: 'backend-down' };
  }

  if (!res.ok) {
    // 404/405: backend is up but nothing is mapped on /graphql yet.
    // Anything else (e.g. the dev proxy's 500 when the port is closed) means no backend.
    return { status: res.status === 404 || res.status === 405 ? 'no-graphql' : 'backend-down' };
  }

  let json;
  try {
    json = await res.json();
  } catch {
    return { status: 'no-graphql' };
  }

  const schema = json?.data?.__schema;
  if (!schema) return { status: 'no-graphql' };

  // typeName -> (fieldName -> Set of arg names)
  const typeFields = new Map();
  // union/interface name -> Set of member (possible) type names
  const possibleTypes = new Map();
  for (const type of schema.types ?? []) {
    if (!type.name || type.name.startsWith('__')) continue;
    const fields = new Map();
    for (const field of type.fields ?? []) {
      fields.set(field.name, new Set((field.args ?? []).map(a => a.name)));
    }
    typeFields.set(type.name, fields);
    if (type.possibleTypes?.length) {
      possibleTypes.set(type.name, new Set(type.possibleTypes.map(t => t.name)));
    }
  }

  const rootName = {
    query: schema.queryType?.name,
    mutation: schema.mutationType?.name,
    subscription: schema.subscriptionType?.name,
  };
  const rootFields = kind => typeFields.get(rootName[kind]) ?? new Map();

  return {
    status: 'ready',
    type: name => typeFields.has(name),
    field: (type, field) => typeFields.get(type)?.has(field) ?? false,
    query: name => rootFields('query').has(name),
    mutation: name => rootFields('mutation').has(name),
    subscription: name => rootFields('subscription').has(name),
    // arg('query', 'movies', 'filter') - does the root field accept this argument?
    arg: (kind, field, arg) => rootFields(kind).get(field)?.has(arg) ?? false,
    // unionMember('SearchResult', 'TvShow') - is a type a member of a union/interface?
    unionMember: (abstractType, member) => possibleTypes.get(abstractType)?.has(member) ?? false,
  };
}
