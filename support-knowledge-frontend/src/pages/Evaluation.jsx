import { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";

function Evaluation() {

    const [evaluation, setEvaluation] = useState(null);

    useEffect(() => {

        fetch("http://localhost:8080/api/evaluate")
            .then((response) => response.json())
            .then((data) => setEvaluation(data))
            .catch(() => setEvaluation(null));

    }, []);

    return (
        <div className="app-layout">

            <Sidebar />

            <main className="main-content">

                <Header
                    title="Evaluation"
                    subtitle="Retrieval quality and system metrics"
                />

                <div className="page-container">

                    <div className="page-heading">

                        <div>
                            <h1>Retrieval Evaluation</h1>

                            <p>
                                Monitor how effectively the system
                                retrieves relevant knowledge.
                            </p>
                        </div>

                    </div>

                    <div className="metric-grid">

                        <div className="metric-card">
                            <span>Questions</span>
                            <strong>
                                {evaluation?.totalQuestions ?? "--"}
                            </strong>
                        </div>

                        <div className="metric-card">
                            <span>Correct Retrievals</span>
                            <strong>
                                {evaluation?.correctRetrievals ?? "--"}
                            </strong>
                        </div>

                        <div className="metric-card">
                            <span>Failed Retrievals</span>
                            <strong>
                                {evaluation?.failedRetrievals ?? "--"}
                            </strong>
                        </div>

                        <div className="metric-card">
                            <span>Recall@5</span>
                            <strong>
                                {evaluation?.recallAt5 != null
                                    ? `${Math.round(
                                          evaluation.recallAt5 * 100
                                      )}%`
                                    : "--"}
                            </strong>
                        </div>

                    </div>

                    <div className="evaluation-info">

                        <span>✓</span>

                        <div>
                            <strong>
                                Retrieval evaluation
                            </strong>

                            <p>
                                These metrics are calculated from
                                the evaluation dataset configured
                                in the Spring Boot backend.
                            </p>
                        </div>

                    </div>

                </div>

            </main>

        </div>
    );
}

export default Evaluation;