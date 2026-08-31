import type { PaymentSearch, RegistrationSearch } from "@/lib/dashboard/api";
import {
  paymentMethods,
  paymentStatuses,
  registrationStatuses,
} from "@/lib/dashboard/status";
import { parsePageParam, parseSizeParam } from "@/lib/utils/pagination";

export function parseRegistrationSearch(params: {
  category?: string;
  direction?: string;
  page?: string;
  q?: string;
  size?: string;
  sortBy?: string;
  status?: string;
}): RegistrationSearch {
  return {
    category: textParam(params.category),
    direction: directionParam(params.direction),
    page: parsePageParam(params.page),
    q: textParam(params.q),
    size: parseSizeParam(params.size, 20),
    sortBy: registrationSortParam(params.sortBy),
    status: enumParam(params.status, registrationStatuses),
  };
}

export function parsePaymentSearch(params: {
  direction?: string;
  method?: string;
  page?: string;
  q?: string;
  size?: string;
  sortBy?: string;
  status?: string;
}): PaymentSearch {
  return {
    direction: directionParam(params.direction),
    method: enumParam(params.method, paymentMethods),
    page: parsePageParam(params.page),
    q: textParam(params.q),
    size: parseSizeParam(params.size, 20),
    sortBy: paymentSortParam(params.sortBy),
    status: enumParam(params.status, paymentStatuses),
  };
}

function enumParam<T extends string>(
  value: string | undefined,
  values: readonly T[]
) {
  return values.includes(value as T) ? (value as T) : undefined;
}

function textParam(value?: string) {
  return value?.trim() || undefined;
}

function directionParam(value?: string) {
  return value === "asc" ? "asc" : "desc";
}

function registrationSortParam(value?: string) {
  return value === "status" || value === "createdAt" ? value : "registeredAt";
}

function paymentSortParam(value?: string) {
  return value === "paidAt" || value === "status" ? value : "createdAt";
}
