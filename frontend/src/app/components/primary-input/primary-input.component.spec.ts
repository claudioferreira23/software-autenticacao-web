import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrimaryInputComponent } from './primary-input.component';

describe('PrimaryInputComponent', () => {
  let component: PrimaryInputComponent;
  let fixture: ComponentFixture<PrimaryInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrimaryInputComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PrimaryInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should propagate input and touched events', () => {
    const onChange = vi.fn();
    const onTouched = vi.fn();
    component.registerOnChange(onChange);
    component.registerOnTouched(onTouched);

    component.onInput({ target: { value: 'maria@exemplo.com' } } as unknown as Event);
    component.onBlur();

    expect(onChange).toHaveBeenCalledWith('maria@exemplo.com');
    expect(onTouched).toHaveBeenCalledOnce();
  });

  it('should reflect the disabled state in the native input', () => {
    component.setDisabledState(true);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.disabled).toBe(true);
  });

  it('should display accessible visual feedback for an invalid field', () => {
    fixture.componentRef.setInput('inputName', 'email');
    fixture.componentRef.setInput('invalid', true);
    fixture.componentRef.setInput('errorMessage', 'Informe um e-mail válido.');
    fixture.detectChanges();

    const wrapper = fixture.nativeElement.querySelector('.input-wrapper') as HTMLElement;
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    const error = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;

    expect(wrapper.classList.contains('is-invalid')).toBe(true);
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe('email-error');
    expect(error.id).toBe('email-error');
    expect(error.textContent).toContain('Informe um e-mail válido.');
  });

  it('should toggle password visibility without changing its value', () => {
    fixture.componentRef.setInput('type', 'password');
    component.writeValue('Senha-Correta-123');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    const toggle = fixture.nativeElement.querySelector('.password-toggle') as HTMLButtonElement;

    expect(input.type).toBe('password');
    expect(toggle.getAttribute('aria-label')).toBe('Mostrar senha');

    toggle.click();
    fixture.detectChanges();

    expect(input.type).toBe('text');
    expect(input.value).toBe('Senha-Correta-123');
    expect(toggle.getAttribute('aria-label')).toBe('Ocultar senha');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');
  });
});
