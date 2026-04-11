import { create } from "zustand";
import { Transaction, Category, MonthlySummary, CategorySummary } from "@/types";
import { transactionApi, categoryApi, summaryApi } from "@/lib/api";
import { format } from "date-fns";

interface TransactionFilters {
  month: string;
  type: string;
}

interface TransactionState {
  transactions: Transaction[];
  categories: Category[];
  summary: MonthlySummary | null;
  categorySummary: CategorySummary[];
  filters: TransactionFilters;
  isLoading: boolean;
  error: string | null;

  setFilters: (filters: Partial<TransactionFilters>) => void;
  fetchTransactions: () => Promise<void>;
  fetchCategories: () => Promise<void>;
  fetchSummary: () => Promise<void>;
  fetchCategorySummary: () => Promise<void>;
  createTransaction: (data: {
    categoryId: number;
    amount: number;
    description?: string;
    transactionDate: string;
    type: string;
  }) => Promise<void>;
  deleteTransaction: (id: number) => Promise<void>;
  createCategory: (data: { name: string; type: string }) => Promise<void>;
  deleteCategory: (id: number) => Promise<void>;
}

export const useTransactionStore = create<TransactionState>((set, get) => ({
  transactions: [],
  categories: [],
  summary: null,
  categorySummary: [],
  filters: {
    month: format(new Date(), "yyyy-MM"),
    type: "",
  },
  isLoading: false,
  error: null,

  setFilters: (filters) => {
    set((state) => ({ filters: { ...state.filters, ...filters } }));
  },

  fetchTransactions: async () => {
    set({ isLoading: true, error: null });
    try {
      const { filters } = get();
      const params: Record<string, string> = { month: filters.month };
      if (filters.type) params.type = filters.type;
      const response = await transactionApi.getAll(params);
      set({ transactions: response.data, isLoading: false });
    } catch {
      set({ error: "Failed to fetch transactions", isLoading: false });
    }
  },

  fetchCategories: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await categoryApi.getAll();
      set({ categories: response.data, isLoading: false });
    } catch {
      set({ error: "Failed to fetch categories", isLoading: false });
    }
  },

  fetchSummary: async () => {
    set({ isLoading: true, error: null });
    try {
      const { filters } = get();
      const response = await summaryApi.monthly(filters.month);
      set({ summary: response.data, isLoading: false });
    } catch {
      set({ error: "Failed to fetch summary", isLoading: false });
    }
  },

  fetchCategorySummary: async () => {
    try {
      const { filters } = get();
      const response = await summaryApi.byCategory(filters.month);
      set({ categorySummary: response.data });
    } catch {
      set({ error: "Failed to fetch category summary" });
    }
  },

  createTransaction: async (data) => {
    set({ isLoading: true, error: null });
    try {
      await transactionApi.create(data);
      await get().fetchTransactions();
      await get().fetchSummary();
      await get().fetchCategorySummary();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Failed to create transaction";
      set({ error: message, isLoading: false });
      throw err;
    }
  },

  deleteTransaction: async (id) => {
    set({ isLoading: true, error: null });
    try {
      await transactionApi.delete(id);
      await get().fetchTransactions();
      await get().fetchSummary();
      await get().fetchCategorySummary();
    } catch {
      set({ error: "Failed to delete transaction", isLoading: false });
    }
  },

  createCategory: async (data) => {
    set({ isLoading: true, error: null });
    try {
      await categoryApi.create(data);
      await get().fetchCategories();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Failed to create category";
      set({ error: message, isLoading: false });
      throw err;
    }
  },

  deleteCategory: async (id) => {
    set({ isLoading: true, error: null });
    try {
      await categoryApi.delete(id);
      await get().fetchCategories();
    } catch {
      set({ error: "Failed to delete category", isLoading: false });
    }
  },
}));
