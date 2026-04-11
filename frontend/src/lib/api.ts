import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "@/store/authStore";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

// Request interceptor — attach JWT
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Access the store directly (outside React components)
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default api;

// Auth endpoints
export const authApi = {
  register: (data: { username: string; email: string; password: string }) =>
    api.post("/auth/register", data),
  login: (data: { username: string; password: string }) =>
    api.post("/auth/login", data),
};

// Category endpoints
export const categoryApi = {
  getAll: () => api.get("/categories"),
  getById: (id: number) => api.get(`/categories/${id}`),
  create: (data: { name: string; type: string }) =>
    api.post("/categories", data),
  update: (id: number, data: { name: string; type: string }) =>
    api.put(`/categories/${id}`, data),
  delete: (id: number) => api.delete(`/categories/${id}`),
};

// Transaction endpoints
export const transactionApi = {
  getAll: (params?: { month?: string; type?: string }) =>
    api.get("/transactions", { params }),
  getById: (id: number) => api.get(`/transactions/${id}`),
  create: (data: {
    categoryId: number;
    amount: number;
    description?: string;
    transactionDate: string;
    type: string;
  }) => api.post("/transactions", data),
  delete: (id: number) => api.delete(`/transactions/${id}`),
};

// Summary endpoints
export const summaryApi = {
  monthly: (month: string) =>
    api.get("/summary/monthly", { params: { month } }),
  byCategory: (month: string) =>
    api.get("/summary/by-category", { params: { month } }),
};
