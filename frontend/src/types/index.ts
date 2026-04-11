export type TransactionType = "INCOME" | "EXPENSE";

export interface User {
  id: number;
  username: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  userId: number;
}

export interface Category {
  id: number;
  name: string;
  type: TransactionType;
}

export interface Transaction {
  id: number;
  categoryId: number;
  categoryName: string;
  amount: number;
  description: string | null;
  transactionDate: string;
  type: TransactionType;
  createdAt: string;
}

export interface TransactionRequest {
  categoryId: number;
  amount: number;
  description?: string;
  transactionDate: string;
  type: TransactionType;
}

export interface MonthlySummary {
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
  transactionCount: number;
}

export interface CategorySummary {
  categoryName: string;
  type: string;
  total: number;
  percentage: number;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  errors?: Record<string, string>;
}
