import { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import MultipleTabsLayout from '../components/MultipleTabsLayout';
import { TitleContext } from '../../../contexts/TitleContext';
import { useLocation, useParams } from 'react-router-dom';
import Parts from './Parts';
import Sets from './Sets';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';

interface PropsType {}

const Inventory = ({}: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const [action, setAction] = useState<() => void>();
  const { setTitle } = useContext(TitleContext);
  const location = useLocation();
  const { partId, setId } = useParams();
  const { hasViewPermission, hasCreatePermission } = useAuth();

  useEffect(() => {
    setTitle(t('Parts_and_Inventory'));
  }, []);

  const arr = location.pathname.split('/');

  const tabs = [
    { value: 'parts', label: t('parts') },
    { value: 'sets', label: t('sets_of_parts') }
  ];
  const minus = partId || setId ? 2 : 1;
  const tabIndex = tabs.findIndex(
    (tab) => tab.value === arr[arr.length - minus]
  );

  if (hasViewPermission(PermissionEntity.PARTS_AND_MULTIPARTS))
    return (
      <MultipleTabsLayout
        basePath={`/app/inventory`}
        tabs={tabs}
        tabIndex={tabIndex}
        title={`Inventory`}
        action={
          hasCreatePermission(PermissionEntity.PARTS_AND_MULTIPARTS)
            ? action
            : null
        }
        actionTitle={tabs[tabIndex].label}
      >
        {tabIndex === 0 && <Parts setAction={setAction} />}
        {tabIndex === 1 && <Sets setAction={setAction} />}
      </MultipleTabsLayout>
    );
  else return <PermissionErrorMessage message={'no_access_inventory'} />;
};

export default Inventory;
