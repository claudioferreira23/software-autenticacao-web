import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('authentication guards', () => {
  let authenticated: boolean;
  let router: { createUrlTree: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authenticated = false;
    router = { createUrlTree: vi.fn().mockReturnValue({} as UrlTree) };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: () => authenticated } },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('should redirect guests away from the protected route', () => {
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(result).not.toBe(true);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
  });

  it('should allow authenticated users into the protected route', () => {
    authenticated = true;

    expect(TestBed.runInInjectionContext(() => authGuard({} as never, {} as never))).toBe(true);
  });

  it('should redirect authenticated users away from guest routes', () => {
    authenticated = true;
    TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never));

    expect(router.createUrlTree).toHaveBeenCalledWith(['/perfil']);
  });

  it('should allow guests into guest routes', () => {
    expect(TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never))).toBe(true);
  });
});
