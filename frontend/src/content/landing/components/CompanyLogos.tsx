import { Container, Box, SxProps, Theme } from '@mui/material';
import { companyLogosAssets } from '../../../utils/overall';
import React from 'react';

export default function CompanyLogos({ sx }: { sx?: SxProps<Theme> }) {
  return (
    <Container maxWidth="lg" sx={sx}>
      <Box
        sx={{
          overflow: 'hidden',
          position: 'relative',
          py: 2, // Add vertical padding
          '&::before, &::after': {
            content: '""',
            position: 'absolute',
            top: 0,
            bottom: 0,
            width: '100px',
            zIndex: 2,
            pointerEvents: 'none'
          },
          '&::before': {
            left: 0,
            background: 'linear-gradient(to right, white, transparent)'
          },
          '&::after': {
            right: 0,
            background: 'linear-gradient(to left, white, transparent)'
          }
        }}
      >
        <Box
          sx={{
            display: 'flex',
            gap: 6,
            alignItems: 'center', // Vertically center items
            height: '60px', // Set explicit height
            animation: 'scroll 30s linear infinite',
            '@keyframes scroll': {
              '0%': { transform: 'translateX(0)' },
              '100%': { transform: 'translateX(-50%)' }
            }
          }}
        >
          {/* Render logos twice for seamless loop */}
          {[...companyLogosAssets, ...companyLogosAssets].map((logo, index) => (
            <Box
              key={index}
              sx={{
                flexShrink: 0,
                display: 'flex',
                alignItems: 'center',
                height: '100%' // Take full height
              }}
            >
              <img
                style={{
                  filter: 'grayscale(100%)',
                  maxHeight: '40px', // Use maxHeight instead of height
                  width: 'auto',
                  objectFit: 'contain'
                }}
                src={logo}
                alt={`company-logo-${index}`}
              />
            </Box>
          ))}
        </Box>
      </Box>
    </Container>
  );
}
