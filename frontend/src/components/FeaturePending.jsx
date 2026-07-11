// Shown in place of a page (or section) whose part of the GraphQL schema has
// not been built yet during the workshop.
export default function FeaturePending({ title, hint }) {
  return (
    <div className="max-w-2xl mx-auto px-4 py-24 text-center">
      <p className="text-4xl mb-4">🚧</p>
      <h1 className="text-2xl font-bold text-white mb-3">{title}</h1>
      {hint && <p className="text-zinc-400 leading-relaxed">{hint}</p>}
      <p className="text-zinc-600 text-sm mt-6">
        This page activates automatically: build the schema for it in the workshop,
        restart the backend, and refresh this page.
      </p>
    </div>
  );
}
