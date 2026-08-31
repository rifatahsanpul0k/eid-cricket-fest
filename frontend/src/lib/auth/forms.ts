import type { BackendProblem } from "@/lib/auth/backend";

export function problemMessage(problem?: BackendProblem) {
  if (!problem) {
    return undefined;
  }

  const validation = problem.errors
    ?.map((error) =>
      error.field ? `${error.field}: ${error.message}` : error.message
    )
    .filter(Boolean)
    .join(" ");

  return validation || problem.detail || problem.title;
}

export function formValue(
  formData: FormData,
  name: string
) {
  const value = formData.get(name);

  return typeof value === "string" ? value.trim() : "";
}
