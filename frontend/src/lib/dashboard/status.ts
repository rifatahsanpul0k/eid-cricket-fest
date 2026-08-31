import type {
  PaymentResponse,
  RegistrationResponse,
} from "@/lib/api/schema-helpers";

export type RegistrationStatus = NonNullable<RegistrationResponse["status"]>;
export type PaymentStatus = NonNullable<PaymentResponse["status"]>;
export type PaymentMethod = NonNullable<PaymentResponse["paymentMethod"]>;

export const registrationStatuses = [
  "PENDING",
  "APPROVED",
  "REJECTED",
  "WITHDRAWN",
] as const satisfies readonly RegistrationStatus[];

export const paymentStatuses = [
  "PENDING",
  "VERIFIED",
  "REJECTED",
  "REFUNDED",
] as const satisfies readonly PaymentStatus[];

export const paymentMethods = [
  "CASH",
  "BKASH",
  "NAGAD",
  "BANK",
  "OTHER",
] as const satisfies readonly PaymentMethod[];

export function registrationStatusLabel(status?: RegistrationStatus) {
  switch (status) {
    case "PENDING":
      return "Pending";
    case "APPROVED":
      return "Approved";
    case "REJECTED":
      return "Rejected";
    case "WITHDRAWN":
      return "Withdrawn";
    default:
      return "Unknown";
  }
}

export function paymentStatusLabel(status?: PaymentStatus) {
  switch (status) {
    case "PENDING":
      return "Pending";
    case "VERIFIED":
      return "Verified";
    case "REJECTED":
      return "Rejected";
    case "REFUNDED":
      return "Refunded";
    default:
      return "Unknown";
  }
}

export function paymentMethodLabel(method?: PaymentMethod) {
  switch (method) {
    case "BKASH":
      return "bKash";
    case "NAGAD":
      return "Nagad";
    case "BANK":
      return "Bank";
    case "CASH":
      return "Cash";
    case "OTHER":
      return "Other";
    default:
      return "Unknown";
  }
}
