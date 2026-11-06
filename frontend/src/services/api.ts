
import axios from 'axios';

const api = axios.create({
  baseURL: '/',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const storedAuth = localStorage.getItem('shivacode_auth');

  if (storedAuth) {
    try {
      const auth = JSON.parse(storedAuth);

      if (auth.token) {
        config.headers.Authorization = `Bearer ${auth.token}`;
      }
    } catch {
      localStorage.removeItem('shivacode_auth');
    }
  }

  return config;
});

export default api;
