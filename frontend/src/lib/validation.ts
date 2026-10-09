import { z } from 'zod';

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),
  password: z
    .string()
    .min(6, 'Password must be at least 6 characters'),
  rememberMe: z.boolean().optional(),
});

export type LoginFormData = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    name: z
      .string()
      .min(2, 'Name must be at least 2 characters')
      .max(50, 'Name must not exceed 50 characters'),
    email: z
      .string()
      .min(1, 'Email is required')
      .email('Please enter a valid email address'),
    password: z
      .string()
      .min(6, 'Password must be at least 6 characters')
      .regex(/[0-9]/, 'Password must include at least one number'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    company: z.string().optional(),
    termsAccepted: z.boolean().refine(val => val === true, {
      message: 'You must accept terms of service to continue',
    }),
  })
  .refine(data => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

export type RegisterFormData = z.infer<typeof registerSchema>;

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),
});

export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

export const customPlatformSchema = z.object({
  name: z.string().min(2, 'Platform name is required'),
  handle: z.string().min(1, 'Username or Handle is required'),
  apiKey: z.string().min(8, 'API Key must be at least 8 characters'),
});

export type CustomPlatformFormData = z.infer<typeof customPlatformSchema>;
