"use client";
import React, { FC, useState, createContext, Dispatch, SetStateAction } from 'react';
type TitleContext = {
  title: string;
  setTitle: Dispatch<SetStateAction<string>>;
};

// eslint-disable-next-line @typescript-eslint/no-redeclare
export const TitleContext = createContext<TitleContext>({} as TitleContext);

export const TitleProvider: FC<{children?: React.ReactNode}> = ({ children }) => {
  const [title, setTitle] = useState<string>('');

  return (
    <TitleContext.Provider value={{ title, setTitle }}>
      {children}
    </TitleContext.Provider>
  );
};

