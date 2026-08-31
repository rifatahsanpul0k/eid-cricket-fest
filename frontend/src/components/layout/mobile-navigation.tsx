"use client";

import Link from "next/link";
import { MenuIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";

const navigation = [
  { label: "Home", href: "/" },
  { label: "Live", href: "/live" },
  { label: "Fixtures", href: "/fixtures" },
  { label: "Standings", href: "/standings" },
  { label: "Teams", href: "/teams" },
  { label: "Players", href: "/players" },
  { label: "Statistics", href: "/statistics" },
  { label: "Knockout", href: "/knockout" },
  { label: "Awards", href: "/awards" },
  { label: "History", href: "/history" },
];

export function MobileNavigation() {
  return (
    <Sheet>
      <SheetTrigger
        render={
          <Button
            aria-label="Open navigation menu"
            className="lg:hidden"
            size="icon"
            variant="outline"
          />
        }
      >
        <MenuIcon />
      </SheetTrigger>
      <SheetContent side="right">
        <SheetHeader>
          <SheetTitle className="font-heading uppercase tracking-normal">
            Eid Cricket Fest
          </SheetTitle>
        </SheetHeader>
        <nav aria-label="Mobile navigation" className="grid gap-1 px-4">
          {navigation.map((item) => (
            <Link
              className="min-h-11 rounded-sm px-3 py-2 text-sm font-medium text-muted-foreground outline-none transition-colors hover:bg-surface-elevated hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
              href={item.href}
              key={item.label}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </SheetContent>
    </Sheet>
  );
}
