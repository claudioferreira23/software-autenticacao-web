import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { ToastrService } from '@openng/ngx-toastr';
import { of } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { ProfileComponent } from './profile.component';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let authService: { currentUser: ReturnType<typeof signal>; logout: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    authService = {
      currentUser: signal({
        id: '7f1c2a54-3b9e-4c1d-9a2f-5e6b7c8d9e0f',
        nome: 'Maria',
        email: 'maria@exemplo.com',
      }),
      logout: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ToastrService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('should render the authenticated identity', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Maria');
    expect(text).toContain('maria@exemplo.com');
  });

  it('should logout and navigate to login', () => {
    component.logout();

    expect(authService.logout).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
