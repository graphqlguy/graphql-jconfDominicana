import { useState } from 'react';
import { useMutation } from '@apollo/client/react';
import { Link, useNavigate } from 'react-router-dom';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import { useAuth } from '../context/AuthContext';
import FeaturePending from '../components/FeaturePending';

export default function LoginPage() {
  const { docs } = useCaps();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const [loginMutation, { loading }] = useMutation(docs.LOGIN ?? NOOP);

  if (!docs.LOGIN) return (
    <FeaturePending
      title="Authentication isn't built yet"
      hint="Add the login mutation during the security class and this page will start working."
    />
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await loginMutation({ variables: { input: form } });
      login(data.login.token, data.login.user);
      navigate('/');
    } catch (err) {
      setError(err.message || 'Invalid username or password');
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-white">Welcome back</h1>
          <p className="text-zinc-400 mt-2">Sign in to your account</p>
        </div>
        <form onSubmit={handleSubmit} className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8 space-y-5">
          {error && (
            <div className="bg-red-900/30 border border-red-800 text-red-400 px-4 py-3 rounded-lg text-sm">
              {error}
            </div>
          )}
          <div>
            <label className="block text-zinc-400 text-sm mb-1.5">Username</label>
            <input
              type="text"
              value={form.username}
              onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
              required
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white placeholder-zinc-500 focus:outline-none focus:border-yellow-500 transition-colors"
              placeholder="Enter username"
            />
          </div>
          <div>
            <label className="block text-zinc-400 text-sm mb-1.5">Password</label>
            <input
              type="password"
              value={form.password}
              onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
              required
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white placeholder-zinc-500 focus:outline-none focus:border-yellow-500 transition-colors"
              placeholder="Enter password"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-yellow-500 hover:bg-yellow-400 disabled:bg-yellow-800 text-black font-semibold py-3 rounded-lg transition-colors"
          >
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
          <div className="text-center">
            <div className="text-zinc-500 text-sm mb-4">Demo accounts:</div>
            <div className="flex gap-3">
              <button type="button" onClick={() => setForm({ username: 'admin', password: 'admin123' })}
                className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm py-2 rounded-lg transition-colors">
                Admin
              </button>
              <button type="button" onClick={() => setForm({ username: 'user', password: 'user123' })}
                className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm py-2 rounded-lg transition-colors">
                User
              </button>
            </div>
          </div>
        </form>
        <p className="text-center text-zinc-500 text-sm mt-6">
          Don't have an account?{' '}
          <Link to="/register" className="text-yellow-400 hover:text-yellow-300">Register</Link>
        </p>
      </div>
    </div>
  );
}
