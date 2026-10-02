import { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";

function Documents() {

    const [documents, setDocuments] = useState([]);

    const [uploading, setUploading] = useState(false);

    const loadDocuments = async () => {

        try {

            const response = await fetch(
                "http://localhost:8080/api/documents"
            );

            const data = await response.json();

            setDocuments(data);

        } catch (error) {

            console.log("Could not load documents");

        }
    };

    useEffect(() => {

        loadDocuments();

    }, []);

    const uploadDocument = async (event) => {

        const file = event.target.files[0];

        if (!file) {
            return;
        }

        const formData = new FormData();

        formData.append("file", file);

        setUploading(true);

        try {

            await fetch(
                "http://localhost:8080/api/documents/upload",
                {
                    method: "POST",
                    body: formData
                }
            );

            await loadDocuments();

        } catch (error) {

            alert("Upload failed");

        } finally {

            setUploading(false);
        }
    };

    const deleteDocument = async (id) => {

        if (!window.confirm("Delete this document?")) {
            return;
        }

        await fetch(
            `http://localhost:8080/api/documents/${id}`,
            {
                method: "DELETE"
            }
        );

        loadDocuments();
    };

    const reindexDocument = async (id) => {

        await fetch(
            `http://localhost:8080/api/documents/${id}/reindex`,
            {
                method: "POST"
            }
        );

        alert("Document re-indexed");
    };

    return (
        <div className="app-layout">

            <Sidebar />

            <main className="main-content">

                <Header
                    title="Documents"
                    subtitle="Manage your knowledge base"
                />

                <div className="page-container">

                    <div className="page-heading">

                        <div>
                            <h1>Your knowledge base</h1>

                            <p>
                                Upload documents that the copilot can
                                search and answer questions from.
                            </p>
                        </div>

                        <label className="upload-button">

                            {uploading
                                ? "Uploading..."
                                : "+ Upload Document"}

                            <input
                                type="file"
                                hidden
                                accept=".pdf,.docx,.txt,.md"
                                onChange={uploadDocument}
                            />

                        </label>

                    </div>

                    <div className="document-grid">

                        {documents.length === 0 ? (

                            <div className="empty-state">

                                <div>📁</div>

                                <h2>
                                    No documents yet
                                </h2>

                                <p>
                                    Upload your first PDF, DOCX,
                                    TXT or Markdown document.
                                </p>

                            </div>

                        ) : (

                            documents.map((document, index) => (

                                <div
                                    className="document-card"
                                    key={document.id || index}
                                >

                                    <div className="document-icon">
                                        📄
                                    </div>

                                    <div className="document-info">

                                        <h3>
                                            {document.title ||
                                                document.fileName ||
                                                "Document"}
                                        </h3>

                                        <p>
                                            {document.source ||
                                                "Knowledge Base"}
                                        </p>

                                    </div>

                                    <div className="document-actions">

                                        <button
                                            onClick={() =>
                                                reindexDocument(
                                                    document.id
                                                )
                                            }
                                        >
                                            ↻
                                        </button>

                                        <button
                                            onClick={() =>
                                                deleteDocument(
                                                    document.id
                                                )
                                            }
                                        >
                                            🗑
                                        </button>

                                    </div>

                                </div>

                            ))

                        )}

                    </div>

                </div>

            </main>

        </div>
    );
}

export default Documents;