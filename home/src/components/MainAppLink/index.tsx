interface MainAppLinkProps {
  children: React.ReactNode;
  href: string;
  params?: Record<string, string>;
}

export default function MainAppLink({ children, href }: MainAppLinkProps) {
  return (
    <a href={href} data-utm-enriched>
      {children}
    </a>
  );
}
