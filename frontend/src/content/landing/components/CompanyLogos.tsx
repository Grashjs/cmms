import { Container, Box, Typography, SxProps, Theme } from '@mui/material';
import { companyLogosAssets } from '../../../utils/overall';
import React from 'react';

export default function CompanyLogos({ sx }: { sx?: SxProps<Theme> }) {
  return (
    <Container maxWidth="lg" sx={sx}>
      <Typography
        variant="body2"
        align="center"
        sx={{
          color: 'text.secondary',
          mb: 1,
          fontWeight: 500,
          letterSpacing: '0.05em',
          textTransform: 'uppercase',
          fontSize: '0.75rem'
        }}
      >
        Used by maintenance teams across multiple industries
      </Typography>

      <Box
        sx={{
          overflow: 'hidden',
          position: 'relative',
          py: 2,
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
            alignItems: 'center',
            height: '60px',
            animation: 'scroll 30s linear infinite',
            '@keyframes scroll': {
              '0%': { transform: 'translateX(0)' },
              '100%': { transform: 'translateX(-50%)' }
            }
          }}
        >
          {[...companyLogosAssets, ...companyLogosAssets].map((logo, index) => (
            <Box
              key={index}
              sx={{
                flexShrink: 0,
                display: 'flex',
                alignItems: 'center',
                height: '100%'
              }}
            >
              <img
                style={{
                  filter: 'grayscale(100%)',
                  maxHeight: '40px',
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
