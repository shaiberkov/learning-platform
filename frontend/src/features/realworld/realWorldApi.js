import { apiGet, apiPost } from "../../lib/api.js";
import { RealWorldPaths } from "../../constants/pages.constants.js";

// Calls for the real-world cards and the simulations gallery (TanStack Query screens).
// The server takes the user from the session cookie, so no user id is sent.

const url = (path) => `${RealWorldPaths.REAL_WORLD_BASE_PATH}${path}`;

export const fetchRealWorldTopics = () => apiGet(url(RealWorldPaths.TOPICS));
export const fetchRealWorldTopic = (topic) => apiGet(url(RealWorldPaths.TOPIC), { topic });
export const saveRealWorldOverride = (body) => apiPost(url(RealWorldPaths.SAVE_OVERRIDE), null, body);
export const resetRealWorldOverride = (topic) => apiPost(url(RealWorldPaths.RESET_OVERRIDE), { topic });
export const fetchExploredSimulations = () => apiGet(url(RealWorldPaths.SIMULATIONS_EXPLORED));
export const recordSimulation = (simulationKey) => apiPost(url(RealWorldPaths.RECORD_SIMULATION), { simulationKey });
