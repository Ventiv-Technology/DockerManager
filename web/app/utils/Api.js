import { create } from 'apisauce';

// define the api
const api = create({
  baseURL: '/api',
  headers: { Accept: 'application/json' },
});

export function setToken(token) {
  api.setHeader('Authorization', `Bearer ${token}`);
}

export default {
  getHostInformation() {
    return api.get('/hosts')
      .then((response) => response.data);
  },
};
