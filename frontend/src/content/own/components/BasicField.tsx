import { Grid, Stack, Typography, Link } from '@mui/material';
import { getPriorityLabel } from '../../../utils/formatters';
import { useTranslation } from 'react-i18next';

interface BasicFieldProps {
  label: string | number;
  value: string | number;
  isPriority?: boolean;
  isLink?: boolean;
  id?: number;
  type?: 'location' | 'asset' | 'team' | 'vendor';
  linkPrefix?: string;
}

const BasicField: React.FC<BasicFieldProps> = ({
  label,
  value,
  isPriority,
  isLink,
  id,
  type,
  linkPrefix
}) => {
  const { t } = useTranslation();
  if (!value) return null;

  const getHref = () => {
    if (type === 'vendor' && id) {
      return `/app/vendors-customers/vendors/${id}`;
    }
    if (type && id) {
      return `/app/${type}s/${id}`;
    }
    if (isLink) {
      const href = value.toString().startsWith('http')
        ? value.toString()
        : `https://${value}`;
      return href;
    }
    return undefined;
  };

  const href = getHref();

  const renderValue = () => {
    if (isPriority) {
      return getPriorityLabel(value.toString(), t);
    }
    return value;
  };

  if (href) {
    const isExternal = isLink || (type === 'vendor' && id);
    return (
      <Grid item xs={12} lg={6}>
        <Typography
          variant="h6"
          sx={{ color: (theme: any) => theme.colors.alpha.black[70] }}
        >
          {label}
        </Typography>
        {isExternal ? (
          <Link
            href={href}
            variant="h6"
            fontWeight="bold"
            target={isLink ? '_blank' : undefined}
            rel={isLink ? 'noopener noreferrer' : undefined}
          >
            {renderValue()}
          </Link>
        ) : (
          <Link href={href} variant="h6" fontWeight="bold">
            {renderValue()}
          </Link>
        )}
      </Grid>
    );
  }

  return (
    <Grid item xs={12} lg={6}>
      <Typography
        variant="h6"
        sx={{ color: (theme: any) => theme.colors.alpha.black[70] }}
      >
        {label}
      </Typography>
      <Typography variant="h6">{renderValue()}</Typography>
    </Grid>
  );
};

export default BasicField;
