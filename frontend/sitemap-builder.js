// file: sitemap-builder.js
require('@babel/register')({
  extensions: ['.ts', '.tsx', '.js', '.jsx'],
  ignore: [/node_modules/],
  presets: [
    '@babel/preset-env',
    '@babel/preset-react',
    '@babel/preset-typescript'
  ]
});

// Make React available globally for JSX transpilation
global.React = require('react');

// Mock browser globals for Node.js environment
global.window = {
  __RUNTIME_CONFIG__: {
    BASE_URL: 'https://atlas-cmms.com'
  },
  location: {
    href: 'https://atlas-cmms.com',
    protocol: 'https:',
    host: 'atlas-cmms.com',
    hostname: 'atlas-cmms.com',
    port: '',
    pathname: '/',
    search: '',
    hash: ''
  },
  navigator: {
    userAgent: 'Node.js',
    language: 'en-US',
    languages: ['en-US', 'en']
  },
  document: {
    documentElement: {
      lang: 'en'
    },
    querySelector: () => null,
    querySelectorAll: () => [],
    cookie: '',
    createElement: () => ({
      setAttribute: () => {},
      getAttribute: () => null,
      style: {}
    }),
    head: {
      appendChild: () => {},
      insertBefore: () => {}
    }
  },
  localStorage: {
    getItem: () => null,
    setItem: () => {},
    removeItem: () => {}
  },
  sessionStorage: {
    getItem: () => null,
    setItem: () => {},
    removeItem: () => {}
  },
  requestAnimationFrame: (callback) => setTimeout(callback, 0),
  cancelAnimationFrame: (id) => clearTimeout(id),
  getComputedStyle: () => ({
    getPropertyValue: () => ''
  })
};

global.document = global.window.document;
global.navigator = global.window.navigator;
global.localStorage = global.window.localStorage;
global.sessionStorage = global.window.sessionStorage;
global.requestAnimationFrame = global.window.requestAnimationFrame;
global.cancelAnimationFrame = global.window.cancelAnimationFrame;

// Function to check if a path should be excluded
function shouldExcludePath(path) {
  const excludedPrefixes = [
    'app',
    'account',
    'oauth2',
    'payment',
    'status',
    'overview'
  ];

  // Exclude wildcard routes or dynamic routes
  if (path.includes('*') || path.includes(':')) {
    return true;
  }

  // Check if path starts with any excluded prefix
  return excludedPrefixes.some(
    (prefix) => path.startsWith(prefix) || path.startsWith(`/${prefix}`)
  );
}

// Function to flatten nested routes
function flattenRoutes(routes, parentPath = '') {
  const flattened = [];

  routes.forEach((route) => {
    const currentPath = route.path || '';
    const fullPath = parentPath
      ? `${parentPath}/${currentPath}`.replace(/\/+/g, '/').replace(/\/$/, '')
      : currentPath || '/';

    // Skip if should be excluded
    if (shouldExcludePath(fullPath)) {
      return;
    }
    // Add current route if it has a path
    if (currentPath && currentPath !== 'index') {
      flattened.push({
        path: fullPath === '' ? '/' : fullPath
      });
    } else if (!currentPath && !parentPath) {
      flattened.push({
        path: '/'
      });
    }

    // Recursively add children (only if parent is not excluded)
    if (route.children && route.children.length > 0) {
      flattened.push(...flattenRoutes(route.children, fullPath));
    }
  });

  return flattened;
}

const router = require('./src/router/index.tsx').default;
const Sitemap = require('react-router-sitemap').default;

// Flatten routes and filter
const flatRoutes = flattenRoutes(router);

// Remove duplicates
const uniqueRoutes = Array.from(new Set(flatRoutes.map((r) => r.path))).map(
  (path) => ({ path })
);

console.log(
  'Routes to include in sitemap:',
  uniqueRoutes.map((r) => r.path)
);
const baseUrl = 'https://atlas-cmms.com';
new Sitemap(uniqueRoutes).build(baseUrl).save('./public/sitemap.xml');

console.log('Sitemap generated successfully!');
console.log(`Total URLs: ${uniqueRoutes.length}`);
const fs = require('fs');
fs.writeFileSync(
  './public/index-now.js',
  `export default ${JSON.stringify(
    uniqueRoutes.map((r) => {
      const { path } = r;
      if (path.startsWith('/')) {
        return baseUrl + path;
      } else return baseUrl + '/' + path;
    }),
    null,
    2
  )}`
);
