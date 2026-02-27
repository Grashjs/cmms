"use client";
import { FC, ReactNode } from 'react';
import PropTypes from 'prop-types';
import { styled } from '@mui/material/styles';

interface LabelProps {
  className?: string;
  color?:
    | 'primary'
    | 'black'
    | 'secondary'
    | 'error'
    | 'warning'
    | 'success'
    | 'info';
  children?: ReactNode;
}

const LabelWrapper = styled('span')(
  ({ theme }) => `
      background-color: ${theme.colors.alpha.black[5]};
      padding: ${theme.spacing(0.5, 1)};
      font-size: ${theme.typography.pxToRem(13)};
      border-radius: ${theme.general.borderRadius};
      display: inline-flex;
      align-items: center;
      justify-content: center;
      max-height: ${theme.spacing(3)};
      
      &.MuiLabel {
        &-primary {
          background-color: ${theme.colors.primary.lighter};
          color: ${theme.palette.primary.main}
        }

        &-black {
          background-color: ${theme.colors.alpha.black[100]};
          color: ${theme.colors.alpha.white[100]};
        }
        
        &-secondary {
          background-color: ${theme.colors.secondary.lighter};
          color: ${theme.palette.secondary.main}
        }
        
        &-success {
          background-color: ${theme.colors.success.lighter};
          color: ${theme.palette.success.main}
        }
        
        &-warning {
          background-color: ${theme.colors.warning.lighter};
          color: ${theme.palette.warning.main}
        }
              
        &-error {
          background-color: ${theme.colors.error.lighter};
          color: ${theme.palette.error.main}
        }
        
        &-info {
          background-color: ${theme.colors.info.lighter};
          color: ${theme.palette.info.main}
        }
      }
`
);

const Label: FC<LabelProps> = ({
  className,
  color = 'secondary',
  children,
  ...rest
}) => {
  return (
    <LabelWrapper className={'MuiLabel-' + color} {...rest}>
      {children}
    </LabelWrapper>
  );
};

Label.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
  color: PropTypes.oneOf([
    'primary',
    'black',
    'secondary',
    'error',
    'warning',
    'success',
    'info'
  ])
};

export default Label;

