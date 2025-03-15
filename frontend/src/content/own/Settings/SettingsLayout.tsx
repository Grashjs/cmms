import { ReactNode, useContext, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import MultipleTabsLayout from '../components/MultipleTabsLayout';
import { TitleContext } from '../../../contexts/TitleContext';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';

interface SettingsLayoutProps {
  children?: ReactNode;
  tabIndex: number;
}

function SettingsLayout(props: SettingsLayoutProps) {
  const { children, tabIndex } = props;
  const { t }: { t: any } = useTranslation();
  const { user } = useAuth();
  const tabs = [
    { value: '', label: t('general_settings') },
    { value: 'work-order', label: t('wo_configuration') },
    { value: 'request', label: t('request_form_configuration') },
    { value: 'roles', label: t('roles') },
    { value: 'checklists', label: t('checklists') },
    { value: 'workflows', label: t('workflows') },
    { value: 'ui-configuration', label: t('ui_configuration') }
  ];
  const { setTitle } = useContext(TitleContext);

  useEffect(() => {
    setTitle(t('settings'));
  }, []);

  return user.role.viewPermissions.includes(PermissionEntity.SETTINGS) ? (
    <MultipleTabsLayout
      basePath="/app/settings"
      tabs={tabs}
      tabIndex={tabIndex}
      title={t('settings')}
    >
      {children}
    </MultipleTabsLayout>
  ) : (
    <PermissionErrorMessage message={'no_access_settings'} />
  );
}

export default SettingsLayout;
