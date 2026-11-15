import { HttpInterceptorFn } from '@angular/common/http';

const ACCESS_TOKEN_KEY = 'df-access-token';

/**
 * Attach the manually supplied WSO2 access token only to same-origin API calls.
 * The token is session-scoped and is never written to localStorage or logged.
 */
export const accessTokenInterceptor: HttpInterceptorFn = (request, next) => {
  if (typeof sessionStorage === 'undefined'
      || !request.url.startsWith('/api/')
      || request.headers.has('Authorization')) {
    return next(request);
  }

  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY)?.trim();
  if (!token) {
    return next(request);
  }

  const normalized = token.replace(/^Bearer\\s+/i, '');
  if (!normalized) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: { Authorization: `Bearer ${normalized}` }
  }));
};
