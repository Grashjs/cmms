import Hero from "./Hero";
import Highlights from "./Highlights";
import NavBar from "src/components/NavBar";
import { Footer } from "src/components/Footer";
import CompanyLogos from "src/components/CompanyLogos";
import { OverviewWrapper } from "./styles";

function Overview() {
  return (
    <OverviewWrapper>
      <NavBar />
      <Hero />
      <CompanyLogos sx={{ mt: { xs: "150px", md: "100px" } }} />
      <Highlights />
      <Footer />
    </OverviewWrapper>
  );
}

export default Overview;
