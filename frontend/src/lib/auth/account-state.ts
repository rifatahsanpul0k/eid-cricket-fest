import type { PaymentResponse, RegistrationResponse } from "@/lib/api/schema-helpers";

export function registrationStatusMessage(
  registration?: RegistrationResponse,
  profileExists = true
) {
  if (!profileExists) {
    return "Create your player profile before registering.";
  }

  if (!registration) {
    return "You are not registered for the current edition yet.";
  }

  switch (registration.status) {
    case "PENDING":
      return "Your registration is pending organizer review.";
    case "APPROVED":
      return "Your registration is approved.";
    case "REJECTED":
      return "Your registration was rejected.";
    case "WITHDRAWN":
      return "Your registration was withdrawn.";
    default:
      return "Registration status is unavailable.";
  }
}

export function shouldOfferPayment(
  registration?: RegistrationResponse,
  payments: PaymentResponse[] = []
) {
  if (!registration || registration.status !== "PENDING") {
    return false;
  }

  const fee = Number(registration.feeAmount ?? 0);

  if (fee <= 0) {
    return false;
  }

  const pendingOrVerified = payments
    .filter((payment) => payment.status === "PENDING" || payment.status === "VERIFIED")
    .reduce((total, payment) => total + Number(payment.amount ?? 0), 0);

  return pendingOrVerified < fee;
}

export function paymentStatusMessage(payments: PaymentResponse[] = []) {
  if (payments.length === 0) {
    return "No payments submitted.";
  }

  const latest = payments[0];

  switch (latest?.status) {
    case "PENDING":
      return "Latest payment is pending verification.";
    case "VERIFIED":
      return "Latest payment is verified.";
    case "REJECTED":
      return "Latest payment was rejected.";
    case "REFUNDED":
      return "Latest payment was refunded.";
    default:
      return "Payment status is unavailable.";
  }
}
