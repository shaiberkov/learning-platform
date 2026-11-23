// The API server, chosen by VITE_API_URL at build time (see frontend/.env.example):
// `npm run dev` reads .env.development (http://localhost:8080); a build without the
// variable talks to the deployed server. No more editing this file to switch.
export const PRODUCTION_API_URL = 'https://learning-project-server.onrender.com';
export const API_BASE_URL = (import.meta.env.VITE_API_URL || PRODUCTION_API_URL).replace(/\/+$/, '');

export const BASE_API = '/Learning-App';
