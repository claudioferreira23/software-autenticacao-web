import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './guards/auth.guard';
import { LoginComponent } from './pages/login/login.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { SignUpComponent } from './pages/signup/signup.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'cadastro',
    component: SignUpComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'perfil',
    component: ProfileComponent,
    canActivate: [authGuard],
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
