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

/**
 * Fully controlled: the selection lives in the form value, not in local state.
 *
 * It used to keep its own copy and mirror it with
 * `useEffect(() => setPartQuantities(selected), [selected])`, which lost the selection in
 * two ways. Picking or deleting parts only wrote to the local copy and never called
 * onChange, so the form value stayed undefined; the caller passes
 * `formik.values[name] ?? []`, so undefined produced a fresh array literal on every
 * render, the dependency changed identity each time, and the effect reset the local copy
 * to empty. Any re-render between choosing parts and typing a quantity therefore wiped
 * the parts — and a purchase order cannot be saved without them.
 */
export default function SelectPartQuantities({
  onChange,
  selected
}: SelectPartQuantitiesProps) {
  const partQuantities = selected ?? [];

  const onPartQuantityChange = (value: string, partQuantity) => {
    onChange(
      partQuantities.map((pq) =>
        pq.id === partQuantity.id ? { ...pq, quantity: Number(value) } : pq
      )
    );
  };

  return (
    <>
      <PartQuantitiesList
        partQuantities={partQuantities}
        onChange={onPartQuantityChange}
        disabled={false}
        onDelete={(partQuantity) =>
          onChange(partQuantities.filter((pq) => pq.id !== partQuantity.id))
        }
      />
      <SelectParts
        selected={partQuantities.map((partQuantity) => partQuantity.part.id)}
        onChange={(newParts) => {
          onChange(
            newParts.map((part) => {
              // Keep the quantity already typed for a part that stays selected —
              // rebuilding every row from scratch reset earlier entries to zero.
              const existing = partQuantities.find(
                (pq) => pq.part.id === part.id
              );
              return (
                existing ?? {
                  part,
                  quantity: 0,
                  id: randomInt(),
                  createdAt: new Date().toDateString(),
                  createdBy: null,
                  updatedAt: null,
                  updatedBy: null
                }
              );
            })
          );
        }}
      />
    </>
  );
}
