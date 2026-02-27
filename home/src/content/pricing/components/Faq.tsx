"use client";
import { Box, Typography } from '@mui/material';
import FaqComponent, { FaqItem } from 'src/components/Faq';
import { useTranslations } from 'next-intl';

export default function Faq() {
  const t = useTranslations();
  const faqItems: FaqItem[] = [
    // {
    //   id: 'panel1',
    //   title: 'Is there a fee for implementation?',
    //   content: (
    //     <Typography variant="body1">
    //       Yes, the pricing depends on the implementation package that best fits
    //       your team's needs. Each package includes setup assistance from a
    //       dedicated Implementation Manager to guide and assist you through
    //       system configuration, data importing, and best practices for a smooth
    //       transition.
    //     </Typography>
    //   )
    // },
    {
      id: 'panel2',
      title: t('pricing.faq_free_users_q'),
      content: (
        <>
          <Typography variant="body1" paragraph>
            {t('pricing.faq_free_users_a1')}
          </Typography>
          <Box component="ol" sx={{ pl: 2 }}>
            <li>
              <Typography variant="body1">
                <strong>{t('pricing.faq_view_only_users_title')}</strong> —
                These users are typically
                {t('pricing.faq_view_only_users_description')}
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>{t('pricing.faq_requester_users_title')}</strong> —
                These users can only submit
                {t('pricing.faq_requester_users_description')}
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>{t('pricing.faq_third_party_users_title')}</strong> —
                These users are typically vendors and contractors. They can't
                actually sign into the
                {t('pricing.faq_third_party_users_description')}
              </Typography>
            </li>
          </Box>
        </>
      )
    },
    {
      id: 'panel3',
      title: t('pricing.faq_paid_users_q'),
      content: (
        <>
          <Typography variant="body1" paragraph>
            {t('pricing.faq_paid_users_a1')}
          </Typography>
          <Box component="ol" sx={{ pl: 2 }}>
            <li>
              <Typography variant="body1">
                <strong>{t('pricing.faq_admin_users_title')}</strong> — These
                users have the ability to
                {t('pricing.faq_admin_users_description')}
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>{t('pricing.faq_technical_users_title')}</strong> —
                These users are typically
                {t('pricing.faq_technical_users_description')}
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>
                  {t('pricing.faq_limited_technical_users_title')}
                </strong>{' '}
                — These users have the
                {t('pricing.faq_limited_technical_users_description')}
              </Typography>
            </li>
          </Box>
        </>
      )
    },
    {
      id: 'panel4',
      title: t('pricing.faq_change_plans_q'),
      content: <Typography>{t('pricing.faq_change_plans_a')}</Typography>
    },
    {
      id: 'panel5',
      title: t('pricing.faq_free_trial_q'),
      content: <Typography>{t('pricing.faq_free_trial_a')}</Typography>
    },
    {
      id: 'panel6',
      title: t('pricing.faq_non_profit_discounts_q'),
      content: (
        <Typography>{t('pricing.faq_non_profit_discounts_a')}</Typography>
      )
    },
    {
      id: 'panel7',
      title: t('pricing.faq_payment_methods_q'),
      content: <Typography>{t('pricing.faq_payment_methods_a')}</Typography>
    },
    {
      id: 'panel8',
      title: t('pricing.faq_cancel_subscription_q'),
      content: <Typography>{t('pricing.faq_cancel_subscription_a')}</Typography>
    },
    {
      id: 'panel9',
      title: t('pricing.faq_data_secure_q'),
      content: <Typography>{t('pricing.faq_data_secure_a')}</Typography>
    }
  ];

  return <FaqComponent items={faqItems} title={t('pricing.faq_title')} />;
}

