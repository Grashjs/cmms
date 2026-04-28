import { useContext, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Outlet, useLocation } from 'react-router-dom';
import MultipleTabsLayout from '../components/MultipleTabsLayout';
import { TitleContext } from '../../../contexts/TitleContext';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';

function SettingsLayout() {
  const { t }: { t: any } = useTranslation();
  const { user } = useAuth();
  const location = useLocation();

  const tabs = [
    { value: '', label: t('general_settings') },
    { value: 'features', label: t('features') },
    { value: 'roles', label: t('roles') },
    { value: 'checklists', label: t('checklists') },
    { value: 'integrations', label: t('integrations') }
  ];

  // Determine the current tab index based on the URL path
  const getCurrentTabIndex = () => {
    return (
      tabs.length -
      1 -
      [...tabs]
        .reverse()
        .findIndex((tab) => location.pathname.includes('/' + tab.value))
    );
  };

  const { setTitle } = useContext(TitleContext);

  useEffect(() => {
    setTitle(t('settings'));
  }, []);

  return user.role.viewPermissions.includes(PermissionEntity.SETTINGS) ? (
    <MultipleTabsLayout
      basePath="/app/settings"
      tabs={tabs}
      tabIndex={getCurrentTabIndex()}
      title={t('settings')}
    >
      <Outlet />
    </MultipleTabsLayout>
  ) : (
    <PermissionErrorMessage message={'no_access_settings'} />
  );
}

export default SettingsLayout;
