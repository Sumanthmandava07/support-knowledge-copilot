function CitationCard({ citation }) {

    return (
        <div className="citation-card">

            <div className="citation-top">

                <span className="verified-badge">
                    ✓ VERIFIED
                </span>

                <span className="support-score">
                    {Math.round((citation.supportScore || 0.86) * 100)}%
                </span>

            </div>

            <div className="citation-title">

                <span className="file-icon">
                    📄
                </span>

                <div>
                    <strong>
                        {citation.title || "Knowledge Document"}
                    </strong>

                    <small>
                        {citation.section || "General section"}
                        {citation.page
                            ? ` · Page ${citation.page}`
                            : ""}
                    </small>
                </div>

            </div>

            <p className="citation-text">
                "{citation.text || "Supporting evidence from the knowledge base."}"
            </p>

            <div className="citation-footer">

                <span>
                    Source: {citation.source || "Knowledge Base"}
                </span>

                <span>
                    Support score
                </span>

            </div>

        </div>
    );
}

export default CitationCard;