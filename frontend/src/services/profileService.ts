import api from './api';
import type { UserProfile } from '../types/profile';

export const getMyProfile = async (): Promise<UserProfile> => {
  const response = await api.get<UserProfile>('/api/users/me/profile');
  return response.data;
};