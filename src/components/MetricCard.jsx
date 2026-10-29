function MetricCard({ label, value }) {
  return (
    <article className="card metric">
      <span className="label">{label}</span>
      <span className="value">{value}</span>
    </article>
  );
}

export default MetricCard;
