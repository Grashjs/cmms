'use client';
import { Container, Box, Typography, SxProps, Theme } from '@mui/material';
import React from 'react';
import { useTranslation } from 'react-i18next';
import { companyLogosAssets } from "../utils/overall";

export default function CompanyLogos({
  sx,
  white,
  compact
}: {
  sx?: SxProps<Theme>;
  white?: boolean;
  compact?: boolean;
}) {
  const { t } = useTranslation();

  return (
    <Container maxWidth="lg" sx={sx}>
      <Typography
        variant="body2"
        align="center"
        sx={{
          color: white ? 'common.white' : 'text.secondary',
          mb: 1,
          fontWeight: 500,
          letterSpacing: '0.05em',
          textTransform: 'uppercase',
          fontSize: white ? '0.55rem' : '0.75rem'
        }}
      >
        {t('trusted_by_maintenance_teams')}
      </Typography>

      <Box
        sx={{
          overflow: 'hidden',
          position: 'relative',
          py: compact ? 1 : 2,
          '&::before, &::after': {
            content: '""',
            position: 'absolute',
            top: 0,
            bottom: 0,
            width: '100px',
            zIndex: 2,
            pointerEvents: 'none'
          },
          '&::before': white
            ? undefined
            : {
                left: 0,
                background: 'linear-gradient(to right, white, transparent)'
              },
          '&::after': white
            ? undefined
            : {
                right: 0,
                background: 'linear-gradient(to left, white, transparent)'
              }
        }}
      >
        <Box
          sx={{
            display: 'flex',
            gap: compact ? 3 : 6,
            alignItems: 'center',
            height: compact ? '40px' : '60px',
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
                  filter: white
                    ? 'brightness(0) invert(1) opacity(0.8)'
                    : 'grayscale(100%)',
                  maxHeight: compact ? '24px' : '40px',
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
