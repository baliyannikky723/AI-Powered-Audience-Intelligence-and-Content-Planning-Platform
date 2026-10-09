import React from 'react';
import { Link } from 'react-router-dom';
import { FileQuestion, Home, Search } from 'lucide-react';
import { Button } from '../../components/ui/Button';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
      <div className="max-w-md w-full text-center space-y-6 bg-white p-8 rounded-2xl border border-slate-200 shadow-sm">
        <div className="w-16 h-16 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto shadow-inner">
          <FileQuestion className="w-8 h-8" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-bold uppercase tracking-widest text-indigo-600 bg-indigo-50 px-2.5 py-1 rounded-full">
            404 Not Found
          </span>
          <h1 className="text-2xl font-extrabold text-slate-900">Page Does Not Exist</h1>
          <p className="text-sm text-slate-500 leading-relaxed">
            The page or route you are looking for has been moved, deleted, or does not exist.
          </p>
        </div>

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link to="/app/overview" className="w-full sm:w-auto">
            <Button variant="primary" size="md" className="w-full">
              <Home className="w-4 h-4 mr-2" />
              Go to Dashboard
            </Button>
          </Link>
          <Link to="/app/comments" className="w-full sm:w-auto">
            <Button variant="secondary" size="md" className="w-full">
              <Search className="w-4 h-4 mr-2" />
              View Comments
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};
