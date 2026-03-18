"use client";

interface MainAppLinkProps {
  children: React.ReactNode;
  href: string;
  params?: Record<string, string>;
}
import { enrichWithClientParams } from "src/utils/urlPaths";

export default function MainAppLink({ children, href }: MainAppLinkProps) {
  const handleClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    window.location.href = enrichWithClientParams(href);
  };

  return (
    <a href={href} onClick={handleClick}>
      {children}
    </a>
  );
}
