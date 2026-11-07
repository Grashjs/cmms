import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useDispatch } from '../../../../store';
import PartQuantity, {
  PartQuantityMiniDTO
} from '../../../../models/owns/partQuantity';
import SelectParts from './SelectParts';
import { randomInt } from 'src/utils/generators';
import PartQuantitiesList from '../PartQuantitiesList';

interface SelectPartQuantitiesProps {
  onChange: (partQuantities: PartQuantityMiniDTO[]) => void;
  selected: PartQuantity[];
}

export default function SelectPartQuantities({
  onChange,
  selected
}: SelectPartQuantitiesProps) {
  const [partQuantities, setPartQuantities] = useState<PartQuantityMiniDTO[]>(
    []
  );

  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const updatePartQuantities = (newPartQuantities: PartQuantityMiniDTO[]) => {
    setPartQuantities(newPartQuantities);
    onChange(newPartQuantities);
  };

  const onPartQuantityChange = (value: string, partQuantity) => {
    updatePartQuantities(
      partQuantities.map((pq) =>
        pq.id === partQuantity.id ? { ...pq, quantity: Number(value) } : pq
      )
    );
  };

  useEffect(() => {
    setPartQuantities(selected);
  }, [selected]);
  return (
    <>
      <PartQuantitiesList
        partQuantities={partQuantities}
        onChange={onPartQuantityChange}
        disabled={false}
        onDelete={(partQuantity) =>
          updatePartQuantities(
            partQuantities.filter((pq) => pq.id !== partQuantity.id)
          )
        }
      />
      <SelectParts
        selected={partQuantities.map((partQuantity) => partQuantity.part.id)}
        onChange={(newParts) => {
          updatePartQuantities(
            newParts.map(
              (part) =>
                partQuantities.find((pq) => pq.part.id === part.id) ?? {
                  part,
                  quantity: 1,
                  id: randomInt(),
                  createdAt: new Date().toDateString(),
                  createdBy: null,
                  updatedAt: null,
                  updatedBy: null
                }
            )
          );
        }}
      />
    </>
  );
}
