
import axios from 'axios';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from '../types/auth';

const API_URL = '/api/auth';

export const registerUser = async (
  data: RegisterRequest
): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(
    `${API_URL}/register`,
    data
  );

  return response.data;
};

export const loginUser = async (
  data: LoginRequest
): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(
    `${API_URL}/login`,
    data
  );

  return response.data;
};
