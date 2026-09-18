// H5 uses the dev-server proxy so the browser reaches the Cloud Studio backend.
// Mini-program and App builds keep the local backend default for device testing.
// #ifdef H5
export const API_BASE = ''
// #endif
// #ifndef H5
export const API_BASE = 'http://127.0.0.1:8080'
// #endif