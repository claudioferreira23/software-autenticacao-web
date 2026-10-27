import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from '@openng/ngx-toastr';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastrService);

  readonly user = this.authService.currentUser;
  readonly isLoggingOut = signal(false);

  logout() {
    if (this.isLoggingOut()) {
      return;
    }

    this.isLoggingOut.set(true);
    this.authService
      .logout()
      .pipe(finalize(() => this.isLoggingOut.set(false)))
      .subscribe({
        next: () => {
          this.toastService.success('Sessão encerrada com sucesso.');
          this.router.navigate(['/login']);
        },
        error: (error: HttpErrorResponse) => {
          const message =
            error.status === 0 || [502, 503, 504].includes(error.status)
              ? 'Não foi possível conectar ao servidor para encerrar a sessão.'
              : 'Não foi possível encerrar a sessão. Tente novamente.';
          this.toastService.error(message);
        },
      });
  }
}
