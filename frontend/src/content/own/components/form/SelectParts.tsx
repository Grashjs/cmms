import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Autocomplete,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  FormGroup,
  Tab,
  Tabs,
  TextField,
  Typography
} from '@mui/material';
import { ChangeEvent, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { PartMiniDTO } from 'src/models/owns/part';
import { useDispatch, useSelector } from '../../../../store';
import ExpandMoreTwoToneIcon from '@mui/icons-material/ExpandMoreTwoTone';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { getPartsMini } from '../../../../slices/part';
import { getMultiParts } from "../../../../slices/multipart";

interface SelectPartsProps {
  onChange: (parts: PartMiniDTO[]) => void;
  selected: number[];
}

export default function SelectParts({ onChange, selected }: SelectPartsProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { partsMini } = useSelector((state) => state.parts);
  const { multiParts } = useSelector((state) => state.multiParts);
  const [currentTab, setCurrentTab] = useState<string>('parts');
  const [selectedParts, setSelectedParts] = useState<PartMiniDTO[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [searchString, setSearchString] = useState<string>('');
  const [openModal, setOpenModal] = useState<boolean>(false);
  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };
  const tabs = [
    { value: 'parts', label: t('parts') },
    { value: 'sets', label: t('sets_of_parts') }
  ];

  useEffect(() => {
    if (partsMini.length) {
      const newSelectedParts = selectedIds
        .map((id) => {
          return partsMini.find((part) => part.id == id);
        })
        .filter((part) => !!part);
      setSelectedParts(newSelectedParts);
    }
  }, [selectedIds, partsMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    dispatch(getPartsMini());
    dispatch(getMultiParts());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
  };
  const onUnSelect = (ids: number[]) => {
    const newSelectedIds = selectedIds.filter((id) => !ids.includes(id));
    setSelectedIds(newSelectedIds);
  };
  return (
    <>
      <Dialog
        fullWidth
        maxWidth="sm"
        open={openModal}
        onClose={() => setOpenModal(false)}
      >
        <DialogTitle
          sx={{
            p: 3
          }}
        >
          <Typography variant="h4" gutterBottom>
            {t('select_parts')}
          </Typography>
        </DialogTitle>
        <DialogContent
          dividers
          sx={{
            p: 3
          }}
        >
          <Tabs
            sx={{ mb: 2 }}
            onChange={handleTabsChange}
            value={currentTab}
            variant="scrollable"
            scrollButtons="auto"
            textColor="primary"
            indicatorColor="primary"
          >
            {tabs.map((tab) => (
              <Tab key={tab.value} label={tab.label} value={tab.value} />
            ))}
          </Tabs>
          {currentTab === 'parts' && (
            <Box>
              <TextField sx={{mb:1}} value={searchString} onChange={(event)=>setSearchString(event.target.value)} placeholder={t('search')} label={t('search')}/>
              <FormGroup>
                {partsMini.filter(part=>part.name.toLowerCase().includes(searchString.toLowerCase())).map((part) => (
                  <FormControlLabel
                    onChange={(event, checked) => {
                      if (checked) {
                        onSelect([part.id]);
                      } else onUnSelect([part.id]);
                    }}
                    key={part.id}
                    control={<Checkbox checked={selectedIds.includes(part.id)} />}
                    label={part.name}
                  />
                ))}
              </FormGroup>
            </Box>
          )}
          {currentTab === 'sets' && (
            <FormGroup>
              {multiParts.map((set) => (
                <FormControlLabel
                  key={set.id}
                  control={
                    <Box display="flex" flexDirection="row" alignItems="center">
                      <Checkbox
                        checked={set.parts.every((part) =>
                          selectedIds.includes(part.id)
                        )}
                        onChange={(event, checked) => {
                          if (checked) {
                            onSelect(set.parts.map((part) => part.id));
                          } else onUnSelect(set.parts.map((part) => part.id));
                        }}
                      />
                      <Accordion>
                        <AccordionSummary
                          expandIcon={<ExpandMoreTwoToneIcon />}
                          aria-controls="panel1a-content"
                        >
                          {set.name}
                        </AccordionSummary>
                        <AccordionDetails>
                          {set.parts.map((part) => (
                            <Typography key={part.id}>{part.name}</Typography>
                          ))}
                        </AccordionDetails>
                      </Accordion>
                    </Box>
                  }
                  label=""
                />
              ))}
            </FormGroup>
          )}
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={() => setOpenModal(false)}>
            {t('cancel')}
          </Button>
          <Button
            variant="contained"
            disabled={!selectedParts.length}
            onClick={() => {
              onChange(selectedParts);
              setOpenModal(false);
            }}
          >
            {t('add_parts')}
          </Button>
        </DialogActions>
      </Dialog>
      <Button
        startIcon={<AddTwoToneIcon />}
        onClick={() => {
          setSelectedIds(selected);
          setOpenModal(true);
        }}
      >
        {t('add_parts')}
      </Button>
    </>
  );
}
