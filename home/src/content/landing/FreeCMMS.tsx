import React from "react";
import FeaturesAlternating from "./FeaturesAlternating";
import NavBar from "src/components/NavBar";
import HeroFree from "./HeroFree";
import Footer from "src/components/Footer";
import { OverviewWrapper } from "./styled";

export default function FreeCMMSPage() {
  return (
    <OverviewWrapper>
      <NavBar />
      <HeroFree />
      <FeaturesAlternating />
      <Footer />
    </OverviewWrapper>
  );
}
