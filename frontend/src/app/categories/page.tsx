"use client";

import { useEffect, useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { useTransactionStore } from "@/store/transactionStore";
import Navbar from "@/components/Navbar";
import { TransactionType } from "@/types";

const DEFAULT_CATEGORIES = [
  { name: "Food & Dining", type: "EXPENSE" as TransactionType },
  { name: "Transport", type: "EXPENSE" as TransactionType },
  { name: "Housing", type: "EXPENSE" as TransactionType },
  { name: "Healthcare", type: "EXPENSE" as TransactionType },
  { name: "Entertainment", type: "EXPENSE" as TransactionType },
  { name: "Salary", type: "INCOME" as TransactionType },
  { name: "Freelance", type: "INCOME" as TransactionType },
  { name: "Investments", type: "INCOME" as TransactionType },
];

export default function CategoriesPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const { categories, isLoading, error, fetchCategories, createCategory, deleteCategory } =
    useTransactionStore();

  const [form, setForm] = useState({ name: "", type: "EXPENSE" as TransactionType });
  const [formError, setFormError] = useState<string | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);
  const [activeFilter, setActiveFilter] = useState<"ALL" | TransactionType>("ALL");

  useEffect(() => {
    if (!isAuthenticated) { router.replace("/login"); return; }
    fetchCategories();
  }, [isAuthenticated, router, fetchCategories]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (!form.name.trim()) { setFormError("Category name is required"); return; }
    try {
      await createCategory({ name: form.name.trim(), type: form.type });
      setForm({ name: "", type: "EXPENSE" });
    } catch {
      setFormError("Failed to create category. It may already exist.");
    }
  };

  const handleQuickAdd = async (name: string, type: TransactionType) => {
    try {
      await createCategory({ name, type });
    } catch {
      // silently skip if already exists
    }
  };

  const filtered = categories.filter(
    (c) => activeFilter === "ALL" || c.type === activeFilter
  );

  const incomeCount = categories.filter((c) => c.type === "INCOME").length;
  const expenseCount = categories.filter((c) => c.type === "EXPENSE").length;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />

      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">Categories</h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left: Add form + quick add */}
          <div className="space-y-4">
            {/* Add Form */}
            <div className="card p-5">
              <h2 className="text-base font-semibold text-gray-900 dark:text-white mb-4">
                New Category
              </h2>

              {formError && (
                <div className="mb-3 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                  <p className="text-xs text-red-600 dark:text-red-400">{formError}</p>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-3">
                <div>
                  <label className="label">Name</label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                    className="input"
                    placeholder="e.g. Groceries"
                    maxLength={100}
                    required
                  />
                </div>
                <div>
                  <label className="label">Type</label>
                  <select
                    value={form.type}
                    onChange={(e) => setForm((f) => ({ ...f, type: e.target.value as TransactionType }))}
                    className="input"
                  >
                    <option value="EXPENSE">Expense</option>
                    <option value="INCOME">Income</option>
                  </select>
                </div>
                <button type="submit" className="btn-primary w-full" disabled={isLoading}>
                  {isLoading ? "Adding..." : "Add Category"}
                </button>
              </form>
            </div>

            {/* Quick Add */}
            <div className="card p-5">
              <h2 className="text-base font-semibold text-gray-900 dark:text-white mb-3">
                Quick Add Defaults
              </h2>
              <div className="flex flex-wrap gap-2">
                {DEFAULT_CATEGORIES.map((cat) => {
                  const exists = categories.some(
                    (c) => c.name.toLowerCase() === cat.name.toLowerCase()
                  );
                  return (
                    <button
                      key={cat.name}
                      onClick={() => !exists && handleQuickAdd(cat.name, cat.type)}
                      disabled={exists}
                      className={`text-xs px-2.5 py-1 rounded-full border transition-colors ${
                        exists
                          ? "border-gray-200 dark:border-gray-700 text-gray-300 dark:text-gray-600 cursor-not-allowed"
                          : cat.type === "INCOME"
                          ? "border-emerald-300 dark:border-emerald-700 text-emerald-700 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-900/20"
                          : "border-red-300 dark:border-red-700 text-red-700 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                      }`}
                    >
                      {exists ? "✓ " : "+ "}
                      {cat.name}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Right: Category list */}
          <div className="lg:col-span-2">
            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
              </div>
            )}

            {/* Filter tabs */}
            <div className="flex gap-2 mb-4">
              {(["ALL", "INCOME", "EXPENSE"] as const).map((f) => (
                <button
                  key={f}
                  onClick={() => setActiveFilter(f)}
                  className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
                    activeFilter === f
                      ? "bg-blue-600 text-white"
                      : "bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 border border-gray-200 dark:border-gray-700"
                  }`}
                >
                  {f === "ALL" ? `All (${categories.length})` : f === "INCOME" ? `Income (${incomeCount})` : `Expense (${expenseCount})`}
                </button>
              ))}
            </div>

            <div className="card overflow-hidden">
              {isLoading ? (
                <div className="flex items-center justify-center h-40">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
                </div>
              ) : filtered.length === 0 ? (
                <div className="text-center py-12 text-gray-400 dark:text-gray-600">
                  <svg className="w-10 h-10 mx-auto mb-2 opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                      d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                  </svg>
                  <p className="text-sm">No categories yet. Add one above.</p>
                </div>
              ) : (
                <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                  {filtered.map((cat) => (
                    <li
                      key={cat.id}
                      className="flex items-center justify-between px-5 py-3.5 hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-8 h-8 rounded-lg flex items-center justify-center text-sm font-bold ${
                            cat.type === "INCOME"
                              ? "bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600"
                              : "bg-red-100 dark:bg-red-900/30 text-red-600"
                          }`}
                        >
                          {cat.name[0].toUpperCase()}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-white">
                            {cat.name}
                          </p>
                          <span className={cat.type === "INCOME" ? "badge-income" : "badge-expense"}>
                            {cat.type}
                          </span>
                        </div>
                      </div>

                      {deleteConfirm === cat.id ? (
                        <span className="flex items-center gap-2 text-sm">
                          <button
                            onClick={() => { deleteCategory(cat.id); setDeleteConfirm(null); }}
                            className="text-red-600 hover:text-red-700 font-medium"
                          >
                            Delete
                          </button>
                          <button
                            onClick={() => setDeleteConfirm(null)}
                            className="text-gray-400 hover:text-gray-600"
                          >
                            Cancel
                          </button>
                        </span>
                      ) : (
                        <button
                          onClick={() => setDeleteConfirm(cat.id)}
                          className="text-gray-300 dark:text-gray-600 hover:text-red-500 dark:hover:text-red-400 transition-colors"
                        >
                          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
