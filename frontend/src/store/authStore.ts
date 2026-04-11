import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import { AuthResponse } from "@/types";

interface AuthState {
  token: string | null;
  user: { id: number; username: string; email: string } | null;
  isAuthenticated: boolean;
  login: (data: AuthResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,

      login: (data: AuthResponse) => {
        set({
          token: data.token,
          user: {
            id: data.userId,
            username: data.username,
            email: data.email,
          },
          isAuthenticated: true,
        });
      },

      logout: () => {
        set({
          token: null,
          user: null,
          isAuthenticated: false,
        });
      },
    }),
    {
      name: "budget-auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
