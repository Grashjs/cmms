"use client";
import { FC } from 'react';
import PropTypes from 'prop-types';

import { useTranslations } from 'next-intl';
import { styled, Typography, Box, Divider } from '@mui/material';

interface PageHeaderProps {
  heading: string;
  subheading: string;
}

const RootWrapper = styled(Box)(
  ({ theme }) => `
        margin-top: ${theme.spacing(5)};
`
);

const PageHeader: FC<PageHeaderProps> = ({ heading, subheading, ...rest }) => {
  const t = useTranslations();

  return (
    <RootWrapper {...rest}>
      {heading && <Typography variant="h1">{t(heading)}</Typography>}
      {subheading && (
        <Typography variant="subtitle2">{t(subheading)}</Typography>
      )}
      <Divider
        sx={{
          mt: 5
        }}
      />
    </RootWrapper>
  );
};

PageHeader.propTypes = {
  heading: PropTypes.string,
  subheading: PropTypes.string
};

export default PageHeader;

