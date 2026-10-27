import { ChangeDetectorRef, Component, Input, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

type InputType = 'text' | 'email' | 'password';

@Component({
  selector: 'app-primary-input',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PrimaryInputComponent),
      multi: true,
    },
  ],
  templateUrl: './primary-input.component.html',
  styleUrl: './primary-input.component.scss',
})
export class PrimaryInputComponent implements ControlValueAccessor {
  @Input() type: InputType = 'text';
  @Input() placeholder = '';
  @Input() label = '';
  @Input() inputName = '';
  @Input() autocomplete = '';
  @Input() invalid = false;
  @Input() errorMessage = '';

  value = '';
  disabled = false;
  passwordVisible = false;

  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  get errorId() {
    return `${this.inputName}-error`;
  }

  get displayedType(): InputType {
    return this.type === 'password' && this.passwordVisible ? 'text' : this.type;
  }

  togglePasswordVisibility() {
    if (this.type === 'password' && !this.disabled) {
      this.passwordVisible = !this.passwordVisible;
    }
  }

  onInput(event: Event) {
    this.value = (event.target as HTMLInputElement).value;
    this.onChange(this.value);
  }

  onBlur() {
    this.onTouched();
  }

  writeValue(value: string | null): void {
    this.value = value ?? '';
    this.changeDetectorRef.markForCheck();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.changeDetectorRef.markForCheck();
  }
}
