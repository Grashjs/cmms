import {
  Box,
  Card,
  CardContent,
  Container,
  Grid,
  Typography
} from '@mui/material';
export interface Testimonial {
  text: string;
  author: string;
  company: string;
}

export default function ({ testimonials }: { testimonials: Testimonial[] }) {
  return (
    <Box
      sx={{
        py: 4
      }}
    >
      <Container maxWidth="lg">
        <Typography variant="h2" align="center" mb={3}>
          Hear it from our customers
        </Typography>
        <Grid container spacing={4}>
          {testimonials.map((testimonial, index) => (
            <Grid item xs={12} md={6} key={index}>
              <Card>
                <CardContent>
                  <Typography variant="body1" paragraph>
                    "{testimonial.text}"
                  </Typography>
                  <Typography variant="subtitle1" align="right">
                    - {testimonial.author}, {testimonial.company}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>
    </Box>
  );
}
